import { fireEvent, render, screen } from '@testing-library/react';
import { DictionaryMultiSelect, DictionarySelect } from './DictionarySelect';
import { ExpectedPositionPicker } from './ExpectedPositionPicker';
import { ExpectedRegionPicker, RegionCascadePicker } from './RegionCascadePicker';

describe('shared selectors', () => {
  it('renders dictionary options and returns stable codes', async () => {
    const onChange = vi.fn();

    render(
      <DictionarySelect
        label="企业性质 *"
        value="legacy_value"
        options={[
          { value: 'state_owned', label: '国企' },
          { value: 'private', label: '民营' }
        ]}
        onChange={onChange}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /legacy_value/ }));
    expect(await screen.findByRole('option', { name: '国企' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('option', { name: '国企' }));

    expect(onChange).toHaveBeenCalledWith('state_owned');
  });

  it('closes dictionary menus when clicking outside', async () => {
    render(
      <div>
        <DictionarySelect
          label="企业性质 *"
          value=""
          options={[
            { value: 'state_owned', label: '国企' },
            { value: 'private', label: '民营' }
          ]}
          onChange={vi.fn()}
        />
        <button type="button">页面其它区域</button>
      </div>
    );

    fireEvent.click(screen.getByRole('button', { name: /请选择/ }));
    expect(await screen.findByRole('option', { name: '国企' })).toBeInTheDocument();

    fireEvent.pointerDown(screen.getByRole('button', { name: '页面其它区域' }));

    expect(screen.queryByRole('option', { name: '国企' })).not.toBeInTheDocument();
  });

  it('supports dictionary multi-select chips and clear action', async () => {
    const onChange = vi.fn();

    render(
      <DictionaryMultiSelect
        label="企业福利"
        values={['five_insurance']}
        options={[
          { value: 'five_insurance', label: '五险一金' },
          { value: 'annual_leave', label: '带薪年假' }
        ]}
        onChange={onChange}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /五险一金/ }));
    expect(await screen.findByRole('option', { name: /带薪年假/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: /带薪年假/ }));

    expect(onChange).toHaveBeenCalledWith(['five_insurance', 'annual_leave']);

    fireEvent.click(screen.getByRole('button', { name: '清空已选' }));

    expect(onChange).toHaveBeenCalledWith([]);
  });

  it('returns a single district code from the region cascade picker', async () => {
    const onChange = vi.fn();

    render(
      <RegionCascadePicker
        mode="single"
        label="所在地区 *"
        value="310000"
        onChange={onChange}
        dialogLabel="所在地选择"
      />
    );

    fireEvent.click(screen.getByRole('button', { name: /上海/ }));
    expect(await screen.findByRole('dialog', { name: '所在地选择' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /河北/ }));
    fireEvent.click(screen.getByRole('button', { name: /唐山/ }));
    fireEvent.click(screen.getByRole('button', { name: '路南区' }));

    expect(onChange).toHaveBeenCalledWith('130202');
  });

  it('shows the full province city district path for single region values', () => {
    render(
      <RegionCascadePicker
        mode="single"
        label="所在地区 *"
        value="140214"
        onChange={vi.fn()}
        dialogLabel="所在地选择"
      />
    );

    expect(screen.getByRole('button', { name: /山西 \/ 大同 \/ 云冈区/ })).toBeInTheDocument();
  });

  it('closes single region cascades when clicking outside', async () => {
    render(
      <div>
        <RegionCascadePicker
          mode="single"
          label="所在地区 *"
          value="310000"
          onChange={vi.fn()}
          dialogLabel="所在地选择"
        />
        <button type="button">表单空白区</button>
      </div>
    );

    fireEvent.click(screen.getByRole('button', { name: /上海/ }));
    expect(await screen.findByRole('dialog', { name: '所在地选择' })).toBeInTheDocument();

    fireEvent.pointerDown(screen.getByRole('button', { name: '表单空白区' }));

    expect(screen.queryByRole('dialog', { name: '所在地选择' })).not.toBeInTheDocument();
  });

  it('keeps multiple region drafts private until save', async () => {
    const onSave = vi.fn();

    render(<ExpectedRegionPicker selectedRegions={[]} selectedCityCode="" onSave={onSave} />);

    fireEvent.click(screen.getByRole('button', { name: /请选择期望地区/ }));
    expect(await screen.findByRole('dialog', { name: /期望地区/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /河北/ }));
    fireEvent.click(screen.getByRole('button', { name: /唐山/ }));
    fireEvent.click(screen.getByRole('button', { name: '路南区' }));
    expect(screen.getByLabelText('已选期望地区')).toHaveTextContent('路南区');

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    expect(onSave).toHaveBeenCalledWith({ regions: ['路南区'], cityCode: '130202' });
  });

  it('dismisses multiple region drafts when clicking the backdrop', async () => {
    const onSave = vi.fn();

    render(<ExpectedRegionPicker selectedRegions={[]} selectedCityCode="" onSave={onSave} />);

    fireEvent.click(screen.getByRole('button', { name: /请选择期望地区/ }));
    const dialog = await screen.findByRole('dialog', { name: /期望地区/ });
    fireEvent.click(screen.getByRole('button', { name: /河北/ }));
    fireEvent.click(screen.getByRole('button', { name: /唐山/ }));
    fireEvent.click(screen.getByRole('button', { name: '路南区' }));
    expect(screen.getByLabelText('已选期望地区')).toHaveTextContent('路南区');

    fireEvent.mouseDown(dialog.parentElement!);

    expect(screen.queryByRole('dialog', { name: /期望地区/ })).not.toBeInTheDocument();
    expect(onSave).not.toHaveBeenCalled();
  });

  it('keeps expected position selection rules in the shared picker', async () => {
    const onSave = vi.fn();

    render(<ExpectedPositionPicker selectedPositions={[]} selectedCategoryCode="" onSave={onSave} />);

    fireEvent.click(screen.getByRole('button', { name: /请选择期望职位/ }));
    expect(await screen.findByRole('dialog', { name: /期望职位/ })).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('服务员'));
    fireEvent.click(screen.getByLabelText('送餐员'));
    expect(screen.getByLabelText('已选期望职位')).toHaveTextContent('服务员');
    expect(screen.getByLabelText('已选期望职位')).toHaveTextContent('送餐员');

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    expect(onSave).toHaveBeenCalledWith({
      positions: ['服务员', '送餐员'],
      categoryCode: 'life_service'
    });
  });

  it('dismisses expected position drafts when clicking the backdrop', async () => {
    const onSave = vi.fn();

    render(<ExpectedPositionPicker selectedPositions={[]} selectedCategoryCode="" onSave={onSave} />);

    fireEvent.click(screen.getByRole('button', { name: /请选择期望职位/ }));
    const dialog = await screen.findByRole('dialog', { name: /期望职位/ });
    fireEvent.click(screen.getByLabelText('服务员'));
    expect(screen.getByLabelText('已选期望职位')).toHaveTextContent('服务员');

    fireEvent.mouseDown(dialog.parentElement!);

    expect(screen.queryByRole('dialog', { name: /期望职位/ })).not.toBeInTheDocument();
    expect(onSave).not.toHaveBeenCalled();
  });
});
